local function to_hash(items)
    local result = {}
    for i = 1, #items, 2 do
        result[items[i]] = items[i + 1]
    end
    return result
end

local newJti = ARGV[1]
local nowIso = ARGV[2]
local refreshTtlSeconds = tonumber(ARGV[3])
local graceWindowSeconds = tonumber(ARGV[4])
local rotationResponseJson = ARGV[5]
local oldJti = ARGV[6]

local family = redis.call('HGETALL', KEYS[1])
if #family == 0 then
    return 'NOT_FOUND'
end

local f = to_hash(family)
if f.revoked == 'true' then
    return 'REVOKED'
end

if nowIso >= f.absoluteExpiry then
    redis.call('HSET', KEYS[1], 'revoked', 'true', 'revokedAt', nowIso, 'reason', 'ABSOLUTE_EXPIRED')
    return 'ABSOLUTE_EXPIRED'
end

local token = redis.call('HGETALL', KEYS[2])
if #token == 0 then
    redis.call('HSET', KEYS[1], 'revoked', 'true', 'revokedAt', nowIso, 'reason', 'REUSE_DETECTED')
    return 'NOT_FOUND'
end

local t = to_hash(token)
if t.status == 'REVOKED' then
    return 'REVOKED'
end

if t.status == 'ROTATED' then
    local cached = redis.call('GET', KEYS[4])
    if cached then
        return 'GRACE:' .. cached
    end
    redis.call('HSET', KEYS[1], 'revoked', 'true', 'revokedAt', nowIso, 'reason', 'REUSE_DETECTED')
    return 'REUSE_DETECTED'
end

redis.call('HSET', KEYS[3],
        'status', 'ACTIVE',
        'issuedAt', nowIso,
        'parentJti', oldJti,
        'nextJti', '')
redis.call('EXPIRE', KEYS[3], refreshTtlSeconds)

redis.call('HSET', KEYS[2], 'status', 'ROTATED', 'nextJti', newJti)
redis.call('SET', KEYS[4], rotationResponseJson, 'EX', graceWindowSeconds)

return 'OK'
