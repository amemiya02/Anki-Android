// SPDX-License-Identifier: GPL-3.0-or-later

const assert = require("node:assert/strict");
const test = require("node:test");
const findBaseline = require("./find_screenshot_baseline.js");

const previous = "a".repeat(40);
const candidate = "b".repeat(40);

function fixture({ payload = { before: previous }, runs = [], artifacts = [] } = {}) {
    const outputs = {};
    const queries = [];
    const options = {
        github: {
            rest: {
                actions: {
                    listWorkflowRuns: async query => {
                        queries.push(query);
                        return { data: { workflow_runs: runs } };
                    },
                    listWorkflowRunArtifacts: async () => ({ data: { artifacts } }),
                },
                repos: {
                    getCommit: async () => ({ data: { parents: [{ sha: previous }] } }),
                },
            },
        },
        context: {
            repo: { owner: "owner", repo: "repo" },
            sha: candidate,
            ref: "refs/heads/main",
            payload,
        },
        core: {
            info: () => {},
            setOutput: (key, value) => {
                outputs[key] = value;
            },
        },
    };
    return { options, outputs, queries };
}

test("a push compares with the previous branch tip", async () => {
    const { options, outputs, queries } = fixture({
        runs: [{ id: 123 }],
        artifacts: [{ name: "screenshot", expired: false }],
    });

    await findBaseline(options);

    assert.equal(queries[0].head_sha, previous);
    assert.equal(queries[0].status, "success");
    assert.equal(queries[0].event, "push");
    assert.deepEqual(outputs, { sha: previous, "run-id": 123 });
});

test("a pull request compares with the SHA from that PR event", async () => {
    const { options, outputs, queries } = fixture({
        payload: { pull_request: { base: { ref: "redesign", sha: previous } } },
        runs: [{ id: 123 }],
        artifacts: [{ name: "screenshot", expired: false }],
    });

    await findBaseline(options);

    assert.equal(queries[0].branch, "redesign");
    assert.equal(queries[0].head_sha, previous);
    assert.deepEqual(outputs, { sha: previous, "run-id": 123 });
});

test("the first push to a branch compares with the first parent", async () => {
    const { options, outputs, queries } = fixture({ payload: { before: "0".repeat(40) } });

    await findBaseline(options);

    assert.equal(queries[0].head_sha, previous);
    assert.deepEqual(outputs, { sha: previous });
});

test("a missing store run requires recording the exact baseline", async () => {
    const { options, outputs, queries } = fixture();

    await findBaseline(options);

    assert.equal(queries.length, 1);
    assert.deepEqual(outputs, { sha: previous });
});

test("expired PNGs and reports alone require recording the exact baseline", async () => {
    const { options, outputs } = fixture({
        runs: [{ id: 123 }],
        artifacts: [
            { name: "screenshot", expired: true },
            { name: "screenshot-reports", expired: false },
        ],
    });

    await findBaseline(options);

    assert.deepEqual(outputs, { sha: previous });
});
