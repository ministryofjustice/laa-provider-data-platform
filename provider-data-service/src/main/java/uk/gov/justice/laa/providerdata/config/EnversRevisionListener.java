package uk.gov.justice.laa.providerdata.config;

import org.hibernate.envers.RevisionListener;
import uk.gov.justice.laa.providerdata.entity.EnversRevisionEntity;

/** Populates custom Envers revision metadata for each audited transaction. */
public class EnversRevisionListener implements RevisionListener {

  @Override
  public void newRevision(Object revisionEntity) {
    if (revisionEntity instanceof EnversRevisionEntity rev) {
      rev.setRevisionUser("SYSTEM");
    }
  }
}
