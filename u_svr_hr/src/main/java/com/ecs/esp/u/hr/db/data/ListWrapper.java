package com.ecs.esp.u.hr.db.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.ecs.msg.rest.custom.RESTMessage;

public class ListWrapper<T> {
    private List<T> items;
    private final RESTMessage msg;

    public ListWrapper(List<T> items, RESTMessage msg) {
        // items가 null인 경우 빈 리스트로 대체
        this.items = (items != null) ? items : new ArrayList<>();
        this.msg = msg;
    }
    
    public List<T> getItems() {
        return items;
    }
    
    public void setItems(List<T> items) {
        // null 체크 후 빈 리스트로 대체하여 안정성 강화
        this.items = (items != null) ? items : new ArrayList<>();
    }
    
    public RESTMessage getRESTMessage() {
        return this.msg;
    }
    
    @Override
    public String toString() {
        // items가 null이 아닌 경우, 각 요소의 toString()을 콤마로 구분하여 하나의 문자열로 생성
        String itemsStr = (items != null)
                ? items.stream()
                       .map(item -> item != null ? item.toString() : "null")
                       .collect(Collectors.joining(", "))
                : "null";
        return "ListWrapper{items=[" + itemsStr + "], RESTMessage=" + msg + "}";
    }
}