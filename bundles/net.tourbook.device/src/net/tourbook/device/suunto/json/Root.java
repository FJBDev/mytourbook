package net.tourbook.device.suunto.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Root {
   @JsonProperty("Samples")
   public List<RootSample> samples;
}
