#!/bin/sh
#
# Copyright 2018 European Commission
#
# Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
# You may not use this work except in compliance with the Licence.
# You may obtain a copy of the Licence at:
#
#     https://joinup.ec.europa.eu/software/page/eupl
#
# Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the Licence for the specific language governing permissions and limitations under the Licence.
#


set -eu

# Check that tag signing works
git tag --sign --message "Dummy Tag" dummy-tag
git tag --delete dummy-tag > /dev/null

# Check GitHub API access token
CLIENT_INFO_URL=https://api.github.com/repos/hypothesis/client
REPO_TMPFILE=/tmp/client-repo.json
curl -s -H "Authorization: Bearer $GITHUB_TOKEN" $CLIENT_INFO_URL > $REPO_TMPFILE
CAN_PUSH=$(node -p -e "perms = require('$REPO_TMPFILE').permissions, perms && perms.push")

if [ "$CAN_PUSH" != "true" ]; then
  echo "Cannot push to GitHub using the access token '$GITHUB_TOKEN'"
  exit 1
fi

# Check that we're not releasing broken code
make test
