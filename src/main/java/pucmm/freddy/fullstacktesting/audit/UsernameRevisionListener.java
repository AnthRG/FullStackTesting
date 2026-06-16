package pucmm.freddy.fullstacktesting.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UsernameRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevision revision = (AuditRevision) revisionEntity;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        revision.setUsername(auth != null ? auth.getName() : "system");
    }
}
