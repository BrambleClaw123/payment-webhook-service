-- KEYS[1] = khóa của xô
-- ARGV[1] = sức chứa tối đa, ARGV[2] = số token nạp thêm mỗi giây
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])

-- Dùng giờ của Redis để nhiều bản app không bị lệch đồng hồ
local t = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

local data = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(data[1])
local ts = tonumber(data[2])

if tokens == nil then       -- lần đầu: xô đầy
	tokens = capacity
	ts = now
end

-- nạp thêm theo thời gian đã trôi qua, không vượt sức chứa
local elapsed = math.max(0, now - ts)
tokens = math.min(capacity, tokens + (elapsed / 1000) * rate)

local allowed = 0
if tokens >= 1 then
	tokens = tokens - 1
	allowed = 1
end

redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
-- xô đầy lại thì giống xô mới, nên cho khóa tự hết hạn để khỏi rác
redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / rate * 1000) + 1000)

return allowed