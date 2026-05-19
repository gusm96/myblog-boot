local mode = ARGV[1]

if mode == 'CHECK' then
    local ttl = redis.call('PTTL', KEYS[1])
    if ttl > 0 then
        return tostring(ttl)
    end
    return '0'
end

if mode == 'FAIL' then
    local windowMs = tonumber(ARGV[2])
    local stages = ARGV[3]

    local count = redis.call('INCR', KEYS[1])
    redis.call('PEXPIRE', KEYS[1], windowMs)

    local existingLockTtl = redis.call('PTTL', KEYS[2])
    if existingLockTtl > 0 then
        return tostring(count) .. ':' .. tostring(existingLockTtl) .. ':0'
    end

    local lockTtlMs = 0
    for failureCount, lockMs in string.gmatch(stages, '(%d+)=(%d+)') do
        if count == tonumber(failureCount) then
            lockTtlMs = tonumber(lockMs)
            break
        end
    end

    if lockTtlMs > 0 then
        redis.call('SET', KEYS[2], '1', 'PX', lockTtlMs)
        return tostring(count) .. ':' .. tostring(lockTtlMs) .. ':1'
    end

    return tostring(count) .. ':0:0'
end

if mode == 'RESET' then
    redis.call('DEL', KEYS[1], KEYS[2])
    return 'OK'
end

return 'UNKNOWN_MODE'
