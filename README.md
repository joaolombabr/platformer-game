# 🎮 Lomba Platformer

Um jogo de plataforma 2D desenvolvido em Java com LibGDX.

## 📸 Screenshot

> *Adicione um screenshot do jogo aqui*

## 🕹️ Como Jogar

| Tecla | Ação |
|-------|------|
| `←` ou `A` | Mover para a esquerda |
| `→` ou `D` | Mover para a direita |
| `↑`, `W` ou `Espaço` | Pular |

## 🛠️ Tecnologias

- **Java 11**
- **LibGDX 1.12.1** — framework de jogos
- **Ashley 1.7.4** — sistema ECS (Entity Component System)
- **Box2D** — física 2D
- **Gradle** — build system

## 📁 Estrutura do Projeto

```
platformer_clean/
├── core/          # Lógica principal do jogo (ECS, sistemas, entidades)
├── desktop/       # Launcher para desktop (Windows/Linux/Mac)
├── android/       # Launcher para Android
└── assets_output/ # Assets do jogo (sprites, mapas, áudio)
```

## ▶️ Como Rodar

### Pré-requisitos
- Java 11 ou superior
- IntelliJ IDEA

### Passos
1. Clone o repositório:
   ```bash
   git clone https://github.com/joaolombabr/platformer-game.git
   ```
2. Abra o IntelliJ IDEA
3. **File → Open** → selecione a pasta `platformer_clean`
4. Aguarde o Gradle sincronizar
5. Rode a configuração `DesktopLauncher`

## ⚙️ Arquitetura

O jogo usa o padrão **ECS (Entity Component System)** com a biblioteca Ashley:

- **Components** — dados puros (posição, física, animação, etc.)
- **Systems** — lógica de jogo (input, movimento, renderização, câmera)
- **Entities** — agrupamento de componentes (jogador, inimigos, itens)

### Sistemas disponíveis
| Sistema | Prioridade | Função |
|---------|-----------|--------|
| `InputSystem` | 1 | Lê teclado e define intenções do jogador |
| `MovementSystem` | 2 | Aplica forças no Box2D |
| `PhysicsSyncSystem` | 3 | Sincroniza posições do Box2D com ECS |
| `AnimationSystem` | 4 | Atualiza frames de animação |
| `RenderSystem` | 5 | Desenha os sprites na tela |
| `CameraSystem` | 6 | Câmera suave seguindo o jogador |
| `CleanupSystem` | 7 | Remove entidades marcadas para exclusão |

## 📜 Licença

Este projeto é de uso livre para fins educacionais.
