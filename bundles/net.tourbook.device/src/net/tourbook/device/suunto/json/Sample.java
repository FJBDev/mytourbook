package net.tourbook.device.suunto.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public class Sample {

   @JsonProperty("GPSAltitude")
   public int    gpsAltitude;

   @JsonProperty("Latitude")
   public double latitude;

   @JsonProperty("Longitude")
   public double longitude;

   @JsonProperty("UTC")
   public ZonedDateTime utc;
}
