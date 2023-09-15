package org.phoebus.channelfinder.entity;

import com.google.common.base.Objects;

import java.util.List;

public class SearchResult {
        private final long count;
        private final List<Channel> channels;
        public SearchResult(List<Channel> channels, long count) {
            this.channels = channels;
            this.count = count;
        }

    @Override
    public String toString() {
        return "SearchResult{" +
                "count=" + count +
                ", channels=" + channels +
                '}';
    }

    @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchResult that = (SearchResult) o;
            return count == that.count && Objects.equal(channels, that.channels);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(count, channels);
        }

        public long getCount() {
            return count;
        }

        public List<Channel> getChannels() {
            return channels;
        }


    }