-- KEYS[1] = redis key
-- ARGV[1] = capacity
-- ARGV[2] = refillRatePerMs
-- ARGV[3] = currentTimeMs

local key = KEYS[1]

local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- Fetch existing bucket state
local value = redis.call("GET", key)

local tokens
local lastRefillTime

if value == false then
    -- First request: initialize bucket full
    tokens = capacity
    lastRefillTime = now
else
    local delimiterIndex = string.find(value, "|")
    tokens = tonumber(string.sub(value, 1, delimiterIndex - 1))
    lastRefillTime = tonumber(string.sub(value, delimiterIndex + 1))
end

-- Calculate elapsed time
local elapsed = now - lastRefillTime

-- Refill tokens
local refill = elapsed * refillRate
tokens = math.min(capacity, tokens + refill)

local allowed = 0
local retryAfterMs = 0

if tokens >= 1 then
    -- Consume one token
    tokens = tokens - 1
    allowed = 1
else
    -- Calculate retry time
    local needed = 1 - tokens
    retryAfterMs = math.ceil(needed / refillRate)
end

-- Update bucket state
local newValue = tokens .. "|" .. now
redis.call("SET", key, newValue)

-- Set TTL = time to fully refill
local ttlSeconds = math.ceil((capacity / refillRate) / 1000)
redis.call("EXPIRE", key, ttlSeconds)

return { allowed, tokens, retryAfterMs }