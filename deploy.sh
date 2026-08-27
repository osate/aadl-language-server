#!/usr/bin/env bash

# Copyright (c) 2004-2026 Carnegie Mellon University and others. (see Contributors file).
# All Rights Reserved.
#
# NO WARRANTY. ALL MATERIAL IS FURNISHED ON AN "AS-IS" BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY
# KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE
# OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT
# MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.
#
# This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
# SPDX-License-Identifier: EPL-2.0
#
# Created, in part, with funding and support from the United States Government. (see Acknowledgments file).
#
# This program includes and/or can make use of certain third party source code, object code, documentation and other
# files ("Third Party Software"). The Third Party Software that is used by this program is dependent upon your system
# configuration. By using this program, You agree to comply with any and all relevant Third Party Software terms and
# conditions contained in any such Third Party Software or separate license file distributed with such Third Party
# Software. The parties who own the Third Party Software ("Third Party Licensors") are intended third party beneficiaries
# to this license with respect to the terms applicable to their Third Party Software. Third Party Software licenses
# only apply to the Third Party Software and not any other portion of this program or this program as a whole.

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repository_dir="${repo_root}/releng/org.osate.aadl.ls.repository/target/repository"
provenance_file="${repo_root}/target/build-provenance.properties"
deploy_dir="${AADL_LS_DEPLOY_DIR:-/var/www/html/download/p2/aadl-language-server}"

if [[ -z "${deploy_dir}" || "${deploy_dir}" == "/path/to/aadl-language-server" || "${deploy_dir}" == "/" ||
		"${deploy_dir}" != /* || "${deploy_dir}" == *"/../"* || "${deploy_dir}" == *"/.." ||
		"${deploy_dir}" == "${repo_root}" || "${deploy_dir}" == "${repo_root}/"* ]]; then
	echo "Set AADL_LS_DEPLOY_DIR or update deploy_dir in deploy.sh before deploying." >&2
	exit 1
fi

if [[ ! -d "${repository_dir}" ]]; then
	echo "Language-server p2 repository not found: ${repository_dir}" >&2
	exit 1
fi

if [[ ! -f "${provenance_file}" ]]; then
	echo "Build provenance not found: ${provenance_file}" >&2
	exit 1
fi

rm -rf "${deploy_dir}"
mkdir -p "${deploy_dir}"
cp -R "${repository_dir}/." "${deploy_dir}/"
cp "${provenance_file}" "${deploy_dir}/build-provenance.properties"
