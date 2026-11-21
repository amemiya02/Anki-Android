// SPDX-License-Identifier: GPL-3.0-or-later

const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { join } = require("node:path");
const test = require("node:test");

const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
const pullRequest = {
    number: 13,
    html_url: "https://github.com/owner/fork/pull/13",
    milestone: { number: 2 },
};

async function runWorkflowScript(workflow) {
    const yaml = readFileSync(join(__dirname, "workflows", workflow), "utf8");
    const script = yaml
        .split("script: |\n")[1]
        .replaceAll("${{ github.event.repository.name }}", "fork")
        .replaceAll("${{ github.event.pull_request.number }}", String(pullRequest.number));
    const updates = [];
    const github = {
        graphql: async (_query, { _url }) => {
            assert.equal(_url, pullRequest.html_url);
            return {
                resource: {
                    closingIssuesReferences: {
                        nodes: [
                            { number: 5, repository: { nameWithOwner: "owner/fork" } },
                            { number: 8, repository: { nameWithOwner: "other/project" } },
                        ],
                    },
                },
            };
        },
        rest: {
            issues: {
                listAssignees: async () => ({ data: [{ login: "maintainer" }] }),
                removeAssignees: async update => updates.push(update),
                update: async update => updates.push(update),
            },
        },
    };
    const context = {
        repo: { owner: "owner", repo: "fork" },
        issue: { number: pullRequest.number },
        payload: { pull_request: pullRequest },
    };
    await new AsyncFunction("github", "context", script)(github, context);
    return updates;
}

test("assignee cleanup loads the fork PR and only edits its repository", async () => {
    const updates = await runWorkflowScript("assignees.yml");
    assert.deepEqual(updates, [
        { owner: "owner", repo: "fork", issue_number: 13, assignees: ["maintainer"] },
        { owner: "owner", repo: "fork", issue_number: 5, assignees: ["maintainer"] },
    ]);
});

test("the PR milestone is carried over to closing issues in its repository", async () => {
    const updates = await runWorkflowScript("milestone.yml");
    assert.deepEqual(updates, [{ owner: "owner", repo: "fork", issue_number: 5, milestone: 2 }]);
});
