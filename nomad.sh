#! /usr/bin/env nix-shell
#! nix-shell -i bash -p nomad
nomad agent -dev -log-level=DEBUG driver.raw_exec.enable=1