# python-3.10.Dockerfile
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache bash ca-certificates curl git build-base \
    openssl-dev zlib-dev bzip2-dev readline-dev sqlite-dev wget llvm \
    ncurses-dev xz-dev tk-dev libffi-dev make python3 py3-pip

RUN adduser -D -s /bin/bash runner
USER runner
WORKDIR /home/runner/app

ENV PYENV_ROOT="/home/runner/.pyenv"
ENV PATH="$PYENV_ROOT/bin:$PYENV_ROOT/shims:$PATH"

RUN curl https://pyenv.run | bash

ENV PYTHON_VERSION=3.10.14
RUN pyenv install -s "$PYTHON_VERSION" && \
    pyenv global "$PYTHON_VERSION"

COPY --chown=runner:runner python/ /home/runner/app/python/
COPY --chown=runner:runner --from=jar evaluation-cli-bin_deploy.jar /home/runner/app/evaluation-cli.jar
COPY --chown=runner:runner evaluation-cli/docker_entrypoint.sh /home/runner/app/entrypoint.sh
RUN chmod +x /home/runner/app/entrypoint.sh

ENV PYTHONUNBUFFERED=1
ENTRYPOINT ["/home/runner/app/entrypoint.sh"]
