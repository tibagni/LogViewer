import re
import os
import glob
import json
import argparse
import requests

from git import Repo
from subprocess import call


def read_credentials():
    try:
        with open("credentials.json") as credentials:
            json_str = credentials.read()
            data = json.loads(json_str)

            if "github_access_token" not in data:
                return None
            if "git_username" not in data:
                return None
            if "git_pwd" not in data:
                return None

            return data
    except:
        return None


def cleanup_old_releases():
    current_releases = glob.glob("../build/libs/*")
    for release in current_releases:
        os.remove(release)


def get_release_notes(repo, last_release_tag):
    commits = list(repo.iter_commits("{}..HEAD".format(last_release_tag)))
    changes = ["* {}".format(c.summary) for c in commits]
    return "\n".join(changes)


def should_continue_release(release_notes, release_version):
    print(
        f"\n\n****Deploying release {release_version} with below changes****")
    print("------------------------------------------------------------")
    print(release_notes + "\n")

    answer = ""
    while answer not in ["y", "n"]:
        answer = input("Proceed with deploy [Y/N]? ").lower()
    return answer == "y"


def update_properties_file(new_release_version):
    with open("../app.properties", "r+") as file:
        text = file.read()
        text = re.sub(r"version=\d+\.?\d*",
                      f"version={new_release_version}", text)
        file.seek(0)
        file.write(text)
        file.truncate()


def commit_version_change(repo, release_version):
    repo.index.add(["src/main/resources/properties/app.properties"])
    repo.index.commit(f"Increment version for release {release_version}")


def build_release(make_jar):
    target = "shadowJar" if make_jar else "build"
    status = call(f"./gradlew {target}", cwd="..", shell=True)
    return status == 0


def test_release():
    status = call("./gradlew test", cwd="..", shell=True)
    return status == 0


def push_changes(repo, tag_name):
    current_dir = os.path.dirname(os.path.abspath(__file__))
    askpass_script = os.path.join(current_dir, 'askpass.py')
    os.environ['GIT_ASKPASS'] = askpass_script

    origin = repo.remote(name='origin')
    origin.push('HEAD')
    origin.push(tag_name)


def create_github_release(access_token, tag_name, release_notes, release_path):
    release_binary_name = f"LogViewer_{tag_name}.jar"

    url = "https://api.github.com/repos/tibagni/LogViewer/releases"
    headers = {
        "Authorization": f"token {access_token}"
    }
    release_content = {
        "tag_name": str(tag_name),
        "name": str(tag_name),
        "body": release_notes
    }

    resp = requests.post(url, json=release_content, headers=headers)
    resp.raise_for_status()

    upload_url = resp.json()["upload_url"]
    upload_url = re.sub(r"{.*}", f"?name={release_binary_name}", upload_url)
    files = {release_binary_name: open(release_path, 'rb')}

    resp = requests.post(upload_url, files=files, headers=headers)
    resp.raise_for_status()

    return resp.json()["url"]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('tag', type=str, help='new release TAG')
    option = parser.parse_args()

    CREDENTIALS = read_credentials() or None
    repo = Repo("..")
    new_version = float(option.tag)
    last_version = float(str(repo.tags[-1]))

    if CREDENTIALS is None:
        print("Missing credentials.json file")
        print(""" 
Credentials file should be in the following format:
{
    "github_access_token": "<github access token>",
    "git_username": "<git username>",
    "git_pwd": "<git password>"
}
        """)
        exit(1)

    if "nothing to commit" not in repo.git.status():
        print("There are uncommitted changes. Aborting...")
        exit(1)

    if new_version <= last_version:
        print(f"New version ({new_version}) is not higher "
              f"than last version ({last_version}). Aborting...")
        exit(1)

    if not test_release():
        print("This release is not passing the tests. Aborting...")
        exit(1)

    if not build_release(make_jar=False):
        print("Releae build failed. Aborting...")
        exit(1)

    print(f"Creating release {new_version}...")

    release_notes = get_release_notes(repo, last_version)
    if not should_continue_release(release_notes, new_version):
        exit(0)

    print("Cleaning up old releases...")
    cleanup_old_releases()

    print(f"Updating version to {new_version}...")
    update_properties_file(new_version)

    print("committing and pushing changes...")
    commit_version_change(repo, new_version)
    repo.create_tag(new_version)
    push_changes(repo, new_version)

    build_release(make_jar=True)
    release_path = glob.glob("../build/libs/*")[0]

    print("Creating new relese on github (this can take a while)...")
    try:
        release_url = create_github_release(
            CREDENTIALS["github_access_token"], new_version, release_notes, release_path)
        print(f"Release {new_version} successfully created: {release_url}")
    except Exception as e:
        print("Failed to create or upload github release. Try manually")
        print(e)
        exit(1)


if __name__ == "__main__":
    main()
