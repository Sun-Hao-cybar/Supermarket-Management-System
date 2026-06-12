-- 为 sys_user 表添加会员等级字段
-- 管理员在员工管理页面为员工分配会员等级
ALTER TABLE supermarket_db.sys_user ADD COLUMN member_level VARCHAR(20) DEFAULT NULL COMMENT '会员等级(VIP/SVIP/普通会员/NULL=无)';
