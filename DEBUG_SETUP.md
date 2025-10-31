# Java Debugging Setup for NBA Standings Viewer

## ✅ Debug Configuration Complete!

Your Docker environment is now configured for Java debugging.

## What Was Configured

### 1. Docker Compose (`docker-compose.yml`)
- Added `JAVA_OPTS` environment variable with debug agent configuration
- Exposed port `5005` for remote debugging
- Debug agent runs in non-suspend mode (app starts immediately)

### 2. Dockerfile
- Modified `ENTRYPOINT` to support `JAVA_OPTS` environment variable
- Debug agent is now loaded when the container starts

### 3. VS Code Launch Configuration (`.vscode/launch.json`)
- Created "Debug NBA Standings Backend (Docker)" configuration
- Configured to attach to `localhost:5005`
- Created "Debug Current Test Class" configuration for test debugging

## How to Use

### Debugging the Running Application

1. **Ensure the backend is running:**
   ```bash
   docker-compose ps backend
   ```
   You should see it's "Up" and healthy.

2. **Open the file you want to debug** (e.g., `StandingsController.java`)

3. **Set breakpoints:**
   - Click in the left margin (gutter) next to line numbers
   - A red dot will appear

4. **Start debugging:**
   - Press `F5` or click the "Run and Debug" icon in the sidebar
   - Select "Debug NBA Standings Backend (Docker)"
   - You should see "Attached to remote target" in the debug console

5. **Trigger your code:**
   - Make an API request: `curl "http://localhost:8080/api/standings?date=2024-10-22&groupBy=DIVISION"`
   - Or run integration tests that hit your endpoints

6. **Debug controls:**
   - `F10` - Step Over
   - `F11` - Step Into
   - `Shift+F11` - Step Out
   - `F5` - Continue
   - `Shift+F5` - Stop Debugging

### Debugging Tests

For debugging integration tests:

1. **Open the test file** (e.g., `StandingsControllerIntegrationTest.java`)

2. **Set breakpoints** in the test method or the code it calls

3. **Right-click on the test method** and select "Debug Test"
   - Or use the debug icon that appears above the test method

### Verifying Debug Setup

Check if the debug agent is running:
```bash
# Check logs for debug message
docker-compose logs backend | grep "Listening for transport"

# Test connection to debug port
nc -zv localhost 5005
```

You should see:
- `Listening for transport dt_socket at address: 5005`
- `Connection to localhost port 5005 [tcp/*] succeeded!`

## Troubleshooting

### Debug port not accessible
```bash
# Restart the backend container
docker-compose restart backend

# Check if port is exposed
docker-compose ps backend
```

### Debugger won't attach
1. Make sure the backend container is running
2. Check that port 5005 is not used by another process:
   ```bash
   lsof -i :5005
   ```
3. Restart VS Code if needed

### Breakpoints not hitting
1. Ensure you're using the correct launch configuration
2. Verify the code you're debugging is actually being executed
3. Check that source code matches the running container (rebuild if needed):
   ```bash
   docker-compose build backend
   docker-compose up -d
   ```

## Debug Configuration Details

### Environment Variables
- `JAVA_OPTS`: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`
  - `transport=dt_socket`: Use socket transport
  - `server=y`: Act as debug server
  - `suspend=n`: Don't wait for debugger to attach (app starts immediately)
  - `address=*:5005`: Listen on all interfaces, port 5005

### Ports
- `8080`: Application HTTP port
- `5005`: Java Debug Wire Protocol (JDWP) port

## Tips

- **Hot Reload**: Changes to Java files require rebuilding the container
- **Inspect Variables**: Hover over variables while debugging to see their values
- **Watch Expressions**: Add expressions to the Watch panel to monitor them
- **Call Stack**: View the execution path in the Call Stack panel
- **Debug Console**: Evaluate expressions on the fly while paused at a breakpoint

## Next Steps

Try debugging your integration tests:
1. Open `StandingsControllerIntegrationTest.java`
2. Set a breakpoint in `testGetStandings_ByDivision_Success()`
3. Right-click the test method and select "Debug Test"
4. Step through the code to see how the request flows through your application

Happy debugging! 🐛🔍
