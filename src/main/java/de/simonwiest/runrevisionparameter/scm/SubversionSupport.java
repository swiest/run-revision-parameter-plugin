package de.simonwiest.runrevisionparameter.scm;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.scm.RevisionParameterAction;
import hudson.scm.SCMRevisionState;
import hudson.scm.SubversionSCM.SvnInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class SubversionSupport implements ScmSupport {

  private static final Logger LOGGER = Logger.getLogger(SubversionSupport.class.getName());

  @Override
  public void addRevisions(String name, AbstractBuild<?, ?> build, EnvVars env, Run run) {
    Map<String, Long> revisions = getRevisions(run.getAction(SCMRevisionState.class));
    if (revisions != null) {
      List<SvnInfo> svnInfos = new ArrayList<SvnInfo>();
      int index = 1;
      for (Entry<String, Long> entry : revisions.entrySet()) {
        String svnUrl = entry.getKey();
        Long svnRevision = entry.getValue();
        LOGGER.fine("Run '" + run + "' used Subversion revision '" + svnUrl + "@" + svnRevision + "'.");

        // Export revision info in environment variables with a counting suffix.
        env.put(name + "_SVN_URL_" + index, svnUrl);
        env.put(name + "_SVN_REVISION_" + index, svnRevision.toString());
        svnInfos.add(new SvnInfo(svnUrl, svnRevision));

        // In addition, first revision info is exported in environment variables without number suffix.
        if (index == 1) {
          env.put(name + "_SVN_URL", svnUrl);
          env.put(name + "_SVN_REVISION", svnRevision.toString());
          svnInfos.add(new SvnInfo(svnUrl, svnRevision));
        }

        // Avoid adding multiple RevisionParameterActions, because this method could me called multiple times during build.
        if (build.getAction(RevisionParameterAction.class) == null) {
          if (!revisions.isEmpty()) {
            build.addAction(new RevisionParameterAction(svnInfos));
          }
        }

        index++;
      }
    } else {
      LOGGER.fine("Found no Subversion SCM revision information in '" + run + "'.");
    }
  }

  /**
   * Returns the SVN revisions contained in a SVNRevisionStatus object.
   * 
   * If hudson.scm.SVNRevisionState would be public, we could use
   * SVNRevisionState.revisions directly. Unfortunately, this is not the case as
   * of subversion-1.51. Until this gets changed, we will have to retrieve the
   * field 'revisions' by a reflection workaround. [JIRA-21907] has been filed.
   * 
   * @param scmState SVNRevisionStatus object, containing SVN revision
   *          information.
   * @return List of revisions (Map of URLs mapped to revision numbers).
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> getRevisions(SCMRevisionState scmState) {

    Map<String, Long> revisions = null;

    try {
      Class<?> clazz = scmState.getClass();
      if ("hudson.scm.SVNRevisionState".equals(clazz.getCanonicalName())) {
        Field field = clazz.getDeclaredField("revisions");
        field.setAccessible(true);
        revisions = (Map<String, Long>) field.get(scmState);
      }
    } catch (IllegalAccessException e) {
      LOGGER.warning("Could not retrieve SVN revisions. Exception raised: " + e.getLocalizedMessage());
    } catch (SecurityException e) {
      LOGGER.warning("Could not retrieve SVN revisions. Exception raised: " + e.getLocalizedMessage());
    } catch (NoSuchFieldException e) {
      LOGGER.warning("Could not retrieve SVN revisions. Exception raised: " + e.getLocalizedMessage());
    }

    return revisions;
  }
}
