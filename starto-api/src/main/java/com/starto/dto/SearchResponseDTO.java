package com.starto.dto;

import com.starto.model.Signal;
import com.starto.model.User;
import java.util.List;

public class SearchResponseDTO {
    private List<User> profiles;
    private List<Signal> signals;

    public SearchResponseDTO() {}

    public List<User> getProfiles() { return profiles; }
    public void setProfiles(List<User> profiles) { this.profiles = profiles; }

    public List<Signal> getSignals() { return signals; }
    public void setSignals(List<Signal> signals) { this.signals = signals; }

    // Manual Builder
    public static SearchResponseDTOBuilder builder() { return new SearchResponseDTOBuilder(); }

    public static class SearchResponseDTOBuilder {
        private SearchResponseDTO instance = new SearchResponseDTO();
        public SearchResponseDTOBuilder profiles(List<User> v) { instance.profiles = v; return this; }
        public SearchResponseDTOBuilder signals(List<Signal> v) { instance.signals = v; return this; }
        public SearchResponseDTO build() { return instance; }
    }
}
