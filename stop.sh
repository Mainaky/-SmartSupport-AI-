#!/bin/bash
echo "Stopping SmartSupport AI..."
docker compose down 2>/dev/null || docker-compose down
echo "All services stopped."
