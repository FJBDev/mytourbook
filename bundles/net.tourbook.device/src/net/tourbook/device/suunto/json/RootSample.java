package net.tourbook.device.suunto.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public class RootSample {
      @JsonProperty("Attributes")
      public String attributes;
      @JsonProperty("Source")
      public String source;
      @JsonProperty("TimeISO8601")
      public ZonedDateTime timeISO8601;
}
