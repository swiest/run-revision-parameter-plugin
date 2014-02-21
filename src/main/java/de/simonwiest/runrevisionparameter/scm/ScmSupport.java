package de.simonwiest.runrevisionparameter.scm;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Run;

public interface ScmSupport {
  /**
   * Scans an existing run for SCM revision information and adds this
   * information as RevisionParameterAction to the current build. Then, the SCM
   * plugin will pick this up and use these revisions for checkout.
   * 
   * @param name Name of Jenkins build parameter.
   * @param build Current build to add RevisionParameterAction to.
   * @param env Environment of current build that will be enriched with
   *          variables containing the revisions to be used. This might come in
   *          handy in build steps.
   * @param run Existing run to scan for SCM information.
   */
  void addRevisions(String name, AbstractBuild<?, ?> build, EnvVars env, Run run);

}
