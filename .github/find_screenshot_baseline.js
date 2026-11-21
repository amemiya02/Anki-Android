// SPDX-License-Identifier: GPL-3.0-or-later

module.exports = async function findScreenshotBaseline({ github, context, core }) {
    const pullRequestBase = context.payload.pull_request?.base;
    const branch = pullRequestBase?.ref ?? context.ref.replace(/^refs\/heads\//, "");
    let sha = pullRequestBase?.sha ?? context.payload.before;
    if (sha === "0".repeat(40)) {
        const {
            data: { parents },
        } = await github.rest.repos.getCommit({ ...context.repo, ref: context.sha });
        sha = parents[0]?.sha;
    }
    if (!/^[0-9a-f]{40}$/.test(sha ?? "") || sha === context.sha) {
        throw new Error(`No comparison base for screenshot candidate ${context.sha}`);
    }
    core.setOutput("sha", sha);

    const {
        data: { workflow_runs: runs },
    } = await github.rest.actions.listWorkflowRuns({
        ...context.repo,
        workflow_id: "screenshot_store.yml",
        branch,
        head_sha: sha,
        event: "push",
        status: "success",
    });
    for (const run of runs) {
        const {
            data: { artifacts },
        } = await github.rest.actions.listWorkflowRunArtifacts({
            ...context.repo,
            run_id: run.id,
            name: "screenshot",
        });
        if (artifacts.some(artifact => artifact.name === "screenshot" && !artifact.expired)) {
            core.info(`Using screenshot baseline for ${sha} from run ${run.id}`);
            core.setOutput("run-id", run.id);
            return;
        }
    }
    core.info(`No stored screenshot baseline for ${sha}. Recording it from source.`);
};
