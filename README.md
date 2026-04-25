### First run

```
podman run -d --name ollama -p 11434:11434 -v ollama:/root/.ollama docker.io/ollama/ollama
podman exec ollama ollama pull qwen3:8b
curl http://localhost:11434/api/generate -d '{"model":"qwen3:8b","prompt":"Hello!","stream":false}'
```

### Subsequent runs

```
podman start ollama
curl http://localhost:11434/api/generate -d '{"model":"qwen3:8b","prompt":"Hello!","stream":false}'
```
