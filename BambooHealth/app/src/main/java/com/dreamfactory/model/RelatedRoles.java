package com.dreamfactory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

import com.dreamfactory.model.Metadata;
import com.dreamfactory.model.RelatedRole;
public class RelatedRoles {
  /* Array of system role records. */
  @JsonProperty("record")
  private List<RelatedRole> record = new ArrayList<RelatedRole>();
  /* Array of metadata returned for GET requests. */
  @JsonProperty("meta")
  private Metadata meta = null;
  public List<RelatedRole> getRecord() {
    return record;
  }
  public void setRecord(List<RelatedRole> record) {
    this.record = record;
  }

  public Metadata getMeta() {
    return meta;
  }
  public void setMeta(Metadata meta) {
    this.meta = meta;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class RelatedRoles {\n");
    sb.append("  record: ").append(record).append("\n");
    sb.append("  meta: ").append(meta).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

