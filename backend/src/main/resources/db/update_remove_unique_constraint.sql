-- 移除 friend_request 表的唯一约束
ALTER TABLE friend_request 
DROP INDEX unique_request;

-- （可选）如果需要，可以添加普通索引来提升查询性能
ALTER TABLE friend_request
ADD INDEX idx_sender (sender_id),
ADD INDEX idx_receiver (receiver_id),
ADD INDEX idx_sender_receiver (sender_id, receiver_id);
