{
  description = "Project dev environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

  outputs =
    { nixpkgs, ... }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "aarch64-darwin"
      ];
      forEachSystem =
        f: nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});
    in
    {
      # `nix develop` (or automatically via direnv)
      devShells = forEachSystem (pkgs: {
        default = pkgs.mkShell {

          # ---- TWEAK PER PROJECT -------------------------------------------
          packages = with pkgs; [
            openjdk21
            jdt-language-server
            maven

            # Node:    nodejs pnpm
            # Python:  python3 uv
            # Go:      go gopls
            # Rust:    cargo rustc rust-analyzer clippy rustfmt
            # Zig:     zig zls
            # Misc:    jq ripgrep postgresql
            # just
          ];

          env = {
            # FOO = "bar";
          };

          shellHook = ''
            # echo "Ready"
          '';
          # ------------------------------------------------------------------
        };
      });

      # `nix fmt` formats all .nix files
      formatter = forEachSystem (pkgs: pkgs.nixfmt-tree);

      # Optional: build your project with `nix build`. Example:
      # packages = forEachSystem (pkgs: {
      #   default = pkgs.buildGoModule {
      #     pname = "app";
      #     version = "0.1.0";
      #     src = ./.;
      #     vendorHash = null;
      #   };
      # });
    };
}
