package com.sauce.sync.requests;

import com.sauce.sync.models.SauceEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sauce on 5/20/15.
 */
public class QueryRequest {

    private List<Filter> filters = new ArrayList<Filter>();
    private long limit = 0;

    public List<Filter> getFilters() {
        return filters;
    }

    public long getLimit() {
        return limit;
    }

    //inject a type filter
    public void setType(String type) {
        if(type == null) {
            return;
        }
        if(filters == null) {
            filters = new ArrayList<Filter>();
        }

        Filter filter = new Filter();
        filter.field = "type";
        filter.operator = "=";
        filter.value = type;
        filters.add(filter);
    }

    public void setFilters(List<Filter> filters) {
        //add the indexed prefix to each filter before adding to the list.  the "Type" filter is unchanged
        for(Filter filter: filters) {
            filter.field = SauceEntity.indexedDataPrefix + '.' + filter.field;
        }

        if(this.filters == null) {
            this.filters = filters;
        } else {
            this.filters.addAll(filters);
        }
    }

    @Override
    public String toString() {
        return "QueryRequest{" +
                "filters=" + filters +
                ", limit=" + limit +
                '}';
    }
    public static class Filter {
        private String field;
        private String operator;
        private Object value;
        private boolean descending = false;

        public String getField() {
            return field;
        }

        public String getOperator() {
            return operator;
        }

        public Object getValue() {
            return value;
        }

        public boolean isDescending() {
            return descending;
        }

        @Override
        public String toString() {
            return "Filter{" +
                    "field='" + field + '\'' +
                    ", operator='" + operator + '\'' +
                    ", value='" + value + '\'' +
                    ", descending=" + descending +
                    '}';
        }
    }
}
