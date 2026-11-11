package com.ketan.payflow.idempotency;

import java.util.UUID;
import java.util.function.Supplier;

import com.ketan.payflow.common.ApiException;
import com.ketan.payflow.common.Hashing;
import com.ketan.payflow.common.IdempotencyKeyReusedException;
import com.ketan.payflow.common.InvalidIdempotencyKeyException;
import com.ketan.payflow.common.RequestInProgressException;
import com.ketan.payflow.config.IdempotencyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final StringRedisTemplate redis;
    private final JsonMapper json;   // Jackson 3: tools.jackson.databind.json.JsonMapper
    private final IdempotencyProperties props;

    public ResponseEntity<?> execute(UUID userId, String key, String path, Object request,
                                     HttpStatus successStatus, Supplier<Object> action) {
        if (key == null || key.isBlank() || key.length() > 100) throw new InvalidIdempotencyKeyException();
        String redisKey = "idem:" + userId + ":" + key;
        String hash = Hashing.sha256Hex(path + json.writeValueAsString(request));

        Boolean claimed = redis.opsForValue().setIfAbsent(
                redisKey,
                json.writeValueAsString(IdempotencyRecord.inProgress(hash)),
                props.inProgressTtl());
        if (!Boolean.TRUE.equals(claimed)) {
            return replay(redisKey, hash);
        }

        try {
            Object body = action.get();
            save(redisKey, IdempotencyRecord.completed(hash, successStatus.value(), json.writeValueAsString(body)));
            return ResponseEntity.status(successStatus).body(body);
        } catch (ApiException e) {
            // A business error is a final answer: a retry must get the same answer
            save(redisKey, IdempotencyRecord.completed(hash, e.status().value(),
                    json.writeValueAsString(e.toProblem(path))));
            throw e;
        } catch (RuntimeException e) {
            redis.delete(redisKey);   // unexpected failure: allow a retry
            throw e;
        }
    }

    private ResponseEntity<?> replay(String redisKey, String hash) {
        String raw = redis.opsForValue().get(redisKey);
        if (raw == null) throw new RequestInProgressException();   // expired between our two calls
        IdempotencyRecord existing = json.readValue(raw, IdempotencyRecord.class);
        if (!existing.requestHash().equals(hash)) throw new IdempotencyKeyReusedException();
        if (!existing.isCompleted()) throw new RequestInProgressException();

        MediaType type = existing.status() >= 400 ? MediaType.APPLICATION_PROBLEM_JSON : MediaType.APPLICATION_JSON;
        return ResponseEntity.status(existing.status())
                .contentType(type)
                .header("Idempotent-Replayed", "true")
                .body(json.readTree(existing.body()));
    }

    private void save(String redisKey, IdempotencyRecord record) {
        redis.opsForValue().set(redisKey, json.writeValueAsString(record), props.completedTtl());
    }
}
