/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Tom Huybrechts, Geoff Cummings, Simon Wiest
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.simonwiest.runrevisionparameter;

import hudson.Extension;
import hudson.model.AutoCompletionCandidates;
import hudson.model.ParameterValue;
import hudson.model.Result;
import hudson.model.SimpleParameterDefinition;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.Run;
import hudson.util.EnumConverter;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

@SuppressWarnings("serial")
public class RunRevisionParameterDefinition extends SimpleParameterDefinition {

  private final String projectName;
  private final String runId;
  private final RunParameterFilter filter;

  /**
   * Constants that control how Run Parameter is filtered.
   * 
   * @since 1.517
   */
  public enum RunParameterFilter {
    ALL, STABLE, SUCCESSFUL, COMPLETED;

    public String getName() {
      return name();
    }

    static {
      Stapler.CONVERT_UTILS.register(new EnumConverter(), RunParameterFilter.class);
    }
  }

  /**
   * @since 1.517
   */
  @DataBoundConstructor
  public RunRevisionParameterDefinition(String name, String projectName, String description, RunParameterFilter filter) {
    super(name, description);
    this.projectName = projectName;
    this.runId = null;
    this.filter = filter;
  }

  private RunRevisionParameterDefinition(String name, String projectName, String runId, String description, RunParameterFilter filter) {
    super(name, description);
    this.projectName = projectName;
    this.runId = runId;
    this.filter = filter;
  }

  @Override
  public ParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
    if (defaultValue instanceof RunRevisionParameterValue) {
      RunRevisionParameterValue value = (RunRevisionParameterValue) defaultValue;
      return new RunRevisionParameterDefinition(getName(), value.getRunId(), getDescription(), getFilter());
    } else {
      return this;
    }
  }

  @Exported
  public String getProjectName() {
    return projectName;
  }

  public Job getProject() {
    return Jenkins.getInstance().getItemByFullName(projectName, Job.class);
  }

  /**
   * @return The current filter value, if filter is null, returns ALL
   * @since 1.517
   */
  public RunParameterFilter getFilter() {
    if (null == filter) {
      return RunParameterFilter.ALL;
    } else {
      return filter;
    }
  }

  /**
   * @since 1.517
   * @return Returns a list of builds, filtered based on the filter value.
   */
  public RunList getBuilds() {
    // use getFilter() method so we don't have to worry about null filter value.
    switch (getFilter()) {
    case COMPLETED:
      return getProject().getBuilds().overThresholdOnly(Result.ABORTED);
    case SUCCESSFUL:
      return getProject().getBuilds().overThresholdOnly(Result.UNSTABLE);
    case STABLE:
      return getProject().getBuilds().overThresholdOnly(Result.SUCCESS);
    default:
      return getProject().getBuilds();
    }
  }

  @Extension
  public static class DescriptorImpl extends ParameterDescriptor {
    @Override
    public String getDisplayName() {
      return Messages.RunRevisionParameterDefinition_DisplayName();
    }

    @Override
    public ParameterDefinition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
      return req.bindJSON(RunRevisionParameterDefinition.class, formData);
    }

    public AutoCompletionCandidates doAutoCompleteProjectName(@QueryParameter String value) {
      return AutoCompletionCandidates.ofJobNames(Job.class, value, null, Jenkins.getInstance());
    }

  }

  @Override
  public ParameterValue getDefaultParameterValue() {
    if (runId != null) {
      return createValue(runId);
    }

    Run<?, ?> lastBuild = null;

    // use getFilter() so we dont have to worry about null filter value.
    switch (getFilter()) {
    case COMPLETED:
      lastBuild = getProject().getLastCompletedBuild();
      break;
    case SUCCESSFUL:
      lastBuild = getProject().getLastSuccessfulBuild();
      break;
    case STABLE:
      lastBuild = getProject().getLastStableBuild();
      break;
    default:
      lastBuild = getProject().getLastBuild();
      break;
    }

    if (lastBuild != null) {
      return createValue(lastBuild.getExternalizableId());
    } else {
      return null;
    }
  }

  @Override
  public ParameterValue createValue(StaplerRequest req, JSONObject jsonObject) {
    RunRevisionParameterValue value = req.bindJSON(RunRevisionParameterValue.class, jsonObject);
    value.setDescription(getDescription());
    return value;
  }

  public RunRevisionParameterValue createValue(String value) {
    return new RunRevisionParameterValue(getName(), value, getDescription());
  }

}
