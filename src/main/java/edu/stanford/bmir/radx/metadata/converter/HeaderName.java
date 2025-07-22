package edu.stanford.bmir.radx.metadata.converter;

public enum HeaderName {
  CREATOR_NAME("creator_name"),
  ORGANIZATION_ROR("organization_ror"),
  CREATOR_NAME_CLEANED("creator_name_cleaned"),
  CREATOR_GIVEN_NAME_CLEANED("creator_given_name_cleaned"),
  CREATOR_FAMILY_NAME_CLEANED("creator_family_name_cleaned"),
  ORCID_ID("orcid_id"),
  CREATOR_AFFILIATION_ROR("creator_affiliation_ror");

  private final String columnName;

  HeaderName(String columnName) {
    this.columnName = columnName;
  }

  public String getColumnName() {
    return columnName;
  }
}
