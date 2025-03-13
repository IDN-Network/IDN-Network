
FROM ubuntu:24.04
ARG VERSION="dev"
ENV NO_PROXY_CACHE="-o Acquire::BrokenProxy=true -o Acquire::http::No-Cache=true -o Acquire::http::Pipeline-Depth=0"

# Update and install dependencies without using any cache
RUN apt-get update $NO_PROXY_CACHE  && \
  # $NO_PROXY_CACHE must not be used here or otherwise will trigger a hadolint error
  apt-get -o Acquire::BrokenProxy=true -o Acquire::http::No-Cache=true -o Acquire::http::Pipeline-Depth=0 \
    --no-install-recommends -q --assume-yes install openjdk-21-jre-headless=21* adduser=3* && \
  # Clean apt cache  \
  apt-get clean  && \
  rm -rf /var/cache/apt/archives/* /var/cache/apt/archives/partial/*  && \
  rm -rf /var/lib/apt/lists/*  && \
  # Creating a user for idn
  adduser --disabled-password --gecos "" --home /opt/idn idn  && \
  chown idn:idn /opt/idn

USER idn
WORKDIR /opt/idn-evmtool

COPY --chown=idn:idn idn-evmtool /opt/idn-evmtool/

ENV PATH="/opt/idn-evmtool/bin:${PATH}"
ENTRYPOINT ["evmtool"]

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Idn EVMTool" \
      org.label-schema.description="EVM Execution Tool" \
      org.label-schema.url="https://idn.idnecology.org/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/idnecology/idn.git" \
      org.label-schema.vendor="Idn ecology" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"