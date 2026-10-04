package net.yazloysasha.tfcrealworld.util.geography;

public enum GeographyKind {
  CONTINENT("continent"),
  REGION("region"),
  SUBREGION("subregion"),
  WAYPOINT("waypoint");

  private final String path;

  GeographyKind(String path) {
    this.path = path;
  }

  public String path() {
    return path;
  }
}
