package com.supermarket.backend.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;   // 响应码：200=成功，500=失败
    private String msg;     // 提示信息
    private T data;         // 返回数据

    // 成功（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    // 成功（不带数据）
    public static <T> Result<T> success() {
        return success(null);
    }

    // 失败
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.setCode(500);
        r.setMsg(msg);
        return r;
    }
}