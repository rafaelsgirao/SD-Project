{
  inputs = {
    nixpkgs.url = "flake:nixpkgs";
    flake-parts.url = "github:hercules-ci/flake-parts";
    pre-commit-hooks-nix = {
      url = "github:cachix/pre-commit-hooks.nix";
      inputs.nixpkgs.follows = "nixpkgs";
      inputs.nixpkgs-stable.follows = "nixpkgs";
    };
    treefmt-nix = {
      url = "github:numtide/treefmt-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };
  outputs = inputs@{ flake-parts, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      imports = [
        # To import a flake module
        # 1. Add foo to inputs
        # 2. Add foo as a parameter to the outputs function
        # 3. Add here: foo.flakeModule
        inputs.pre-commit-hooks-nix.flakeModule
        # inputs.devenv.flakeModule
        inputs.treefmt-nix.flakeModule

      ];
      flake = {
        # Put your original flake attributes here.
      };
      #systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" "x86_64-darwin" ];
      systems = [
        # systems for which you want to build the `perSystem` attributes
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
      ];
      # perSystem = { config, self', inputs', pkgs, system, ... }: {
      perSystem = { config, pkgs, ... }: {
        # Per-system attributes can be defined here. The self' and inputs'
        # module parameters provide easy access to attributes of the same
        # system.

        # https://tecnico-distsys.github.io/software/index.html
        devShells.default =
          let
            mavenPkg = pkgs.maven;
            javaPkg = pkgs.temurin-bin-17;
          in
          pkgs.mkShell {
            #Add executable packages to the nix-shell environment.
            M2_HOME = "${mavenPkg}";
            JAVA_HOME = "${javaPkg}";
            packages = with pkgs; [
              javaPkg
              mavenPkg
              git


              (pkgs.python3.withPackages (python-pkgs: [
                python-pkgs.grpcio
                python-pkgs.grpcio-tools
              ]))
              ruff
              black
            ];

            shellHook = ''
              # export DEBUG=1
              ${config.pre-commit.installationScript}
            '';
          };
        pre-commit = {
          check.enable = true;
          settings.hooks = {
            actionlint.enable = true;
            treefmt.enable = true;
            commitizen = {
              enable = true;
              description = "Check whether the current commit message follows commiting rules. Allow empty commit messages by default, because they typically indicate to Git that the commit should be aborted.";
              entry = "${pkgs.commitizen}/bin/cz check --allow-abort --commit-msg-file";
              stages = [ "commit-msg" ];

              # pass_filenames = false;
            };
          };
        };
        treefmt.projectRootFile = ./flake.nix;
        treefmt.programs = {
          nixpkgs-fmt.enable = true;
          shfmt.enable = true;
          mdformat.enable = true;
          google-java-format.enable = true;
          deadnix.enable = true;
          statix.enable = true;
          black.enable = true;
          # ruff.enable = true;
        };
      };
    };
}

