#!/bin/bash
# ─────────────────────────────────────────────────────────────
#  SmartSupport AI — One-Click Startup Script
#  Usage: chmod +x run.sh && ./run.sh
# ─────────────────────────────────────────────────────────────

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     SmartSupport AI — Starting Up        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
echo ""

# Check Docker
if ! command -v docker &>/dev/null; then
  echo -e "${RED}✗ Docker not found. Install from https://www.docker.com/get-started${NC}"
  exit 1
fi

if ! command -v docker compose &>/dev/null && ! docker-compose version &>/dev/null 2>&1; then
  echo -e "${RED}✗ Docker Compose not found.${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Docker found${NC}"

# Stop any running containers
echo -e "\n${YELLOW}► Stopping any existing containers...${NC}"
docker compose down 2>/dev/null || docker-compose down 2>/dev/null || true

# Build and start
echo -e "\n${YELLOW}► Building and starting all services...${NC}"
echo -e "  This may take 3-5 minutes on first run (downloading images + building JARs)\n"

docker compose up --build -d 2>/dev/null || docker-compose up --build -d

echo -e "\n${YELLOW}► Waiting for services to become healthy...${NC}"
sleep 15

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║              All Services Are Running!                   ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  API (Ticket Service)   →  http://localhost:8080         ║${NC}"
echo -e "${GREEN}║  AI Classifier          →  http://localhost:5000/health  ║${NC}"
echo -e "${GREEN}║  Kafka UI               →  http://localhost:9090         ║${NC}"
echo -e "${GREEN}║  MailHog (emails)       →  http://localhost:8025         ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  Frontend Dashboard     →  open frontend/index.html      ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  To stop everything:  ${CYAN}docker compose down${NC}"
echo -e "  To view logs:        ${CYAN}docker compose logs -f ticket-service${NC}"
echo ""

# Open frontend if on Mac
if [[ "$OSTYPE" == "darwin"* ]]; then
  open frontend/index.html
fi
