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

  public static GeographyKind fromPath(String path) {
    for (GeographyKind kind : values()) {
      if (kind.path.equals(path)) {
        return kind;
      }
    }
    throw new IllegalArgumentException("Unknown geography kind: " + path);
  }
}
