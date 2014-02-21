package de.simonwiest.runrevisionparameter.scm;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.git.Branch;
import hudson.plugins.git.Revision;
import hudson.plugins.git.RevisionParameterAction;
import hudson.plugins.git.util.BuildData;

import java.util.logging.Logger;

public class GitSupport implements ScmSupport {

  private static final Logger LOGGER = Logger.getLogger(GitSupport.class.getName());

  @Override
  public void addRevisions(String name, AbstractBuild<?, ?> build, EnvVars env, Run run) {
    BuildData buildData = run.getAction(BuildData.class);
    if (buildData != null) {
      int index = 1;
      Revision revision = buildData.getLastBuiltRevision();
      if (revision != null) {
        for (Branch branch : revision.getBranches()) {
          String branchName = branch.getName();
          String sha1 = branch.getSHA1String();
          LOGGER.fine("Run '" + run + "' used Git branch '" + branchName + "' (" + sha1 + ").");

          // Export branch info in environment variables with a counting suffix.
          env.put(name + "_GIT_BRANCH_" + index, branchName);
          env.put(name + "_GIT_COMMIT_" + index, sha1);

          // In addition, first branch info is exported in environment variables without number suffix.
          if (index == 1) {
            env.put(name + "_GIT_BRANCH", branchName);
            env.put(name + "_GIT_COMMIT", sha1);
          }

          index++;
        }

        // Avoid adding multiple RevisionParameterActions, because this method could me called multiple times during build.
        if (build.getAction(RevisionParameterAction.class) == null) {
          build.addAction(new RevisionParameterAction(buildData.getLastBuiltRevision(), false));
        }
      }
    } else {
      LOGGER.fine("Found no Git SCM revision information in '" + run + "'.");
    }
  }
}
