# MCP Server Automatic Startup Integration

## Overview

The MCP (Model Context Protocol) server has been successfully integrated into MyTourBook's application lifecycle. The server will now automatically start when the application launches and gracefully shut down when the application closes.

## Changes Made

### 1. Modified TourbookPlugin.java

#### Added Imports
```java
import net.tourbook.importdata.MCPServerIntegration;
import net.tourbook.importdata.MCPServerManager;
```

#### Updated start() Method
The plugin's `start()` method now calls a new `initializeMCPServer()` method to initialize and start the MCP server during application startup.

#### New initializeMCPServer() Method
```java
private void initializeMCPServer() {
   try {
      final MCPServerManager mcpManager = MCPServerManager.getInstance();
      
      // Initialize the server
      if (!mcpManager.initializeServer()) {
         StatusUtil.logError("Failed to initialize MCP server");
         return;
      }
      
      // Start the server in background
      if (!mcpManager.startServer()) {
         StatusUtil.logError("Failed to start MCP server");
         return;
      }
      
      StatusUtil.logInfo("[MCP Server] MCP Server started successfully for AI integration");
      
   } catch (final Exception e) {
      StatusUtil.logError("Error starting MCP server: " + e.getMessage());
      log("Error starting MCP server", e);
   }
}
```

#### Updated stop() Method
The plugin's `stop()` method now calls a new `shutdownMCPServer()` method to gracefully shut down the MCP server during application shutdown.

#### New shutdownMCPServer() Method
```java
private void shutdownMCPServer() {
   try {
      final MCPServerManager mcpManager = MCPServerManager.getInstance();
      
      if (mcpManager.isServerRunning()) {
         if (mcpManager.stopServer()) {
            StatusUtil.logInfo("[MCP Server] MCP Server stopped successfully");
         } else {
            StatusUtil.logError("Error stopping MCP server");
         }
      }
      
   } catch (final Exception e) {
      StatusUtil.logError("Error during MCP server shutdown: " + e.getMessage());
      log("Error during MCP server shutdown", e);
   }
}
```

## Application Startup Sequence

When MyTourBook starts:

1. **OSGi Bundle Activation**
   - TourbookPlugin.start(BundleContext) is called

2. **Plugin Initialization**
   - Plugin instance is set
   - Bundle context is captured
   - Plugin version is logged

3. **MCP Server Initialization** (NEW)
   - MCPServerManager singleton is obtained
   - Server is initialized with default configuration
   - Server is started in a background thread
   - Success message is logged: "[MCP Server] MCP Server started successfully for AI integration"

4. **Application Ready**
   - MCP server is running and accepting connections
   - AI models can connect via MCP protocol

## Application Shutdown Sequence

When MyTourBook closes:

1. **OSGi Bundle Deactivation**
   - TourbookPlugin.stop(BundleContext) is called

2. **MCP Server Shutdown** (NEW)
   - MCPServerManager is accessed
   - If server is running, it's gracefully stopped
   - All resources are cleaned up
   - Shutdown message is logged

3. **Plugin Cleanup**
   - Plugin instance is cleared
   - Bundle context is cleared
   - Parent class stop() is called

## Logging Output

You will see the following log messages in the MyTourBook console/log file:

**On Startup:**
```
[MCP Server] MCP Server started successfully for AI integration
```

**On Shutdown:**
```
[MCP Server] MCP Server stopped successfully
```

**On Errors:**
```
Failed to initialize MCP server
Failed to start MCP server
Error starting MCP server: [error details]
Error stopping MCP server
Error during MCP server shutdown: [error details]
```

## Error Handling

The implementation includes comprehensive error handling:

- **Initialization Errors** - Logged and caught gracefully
- **Startup Errors** - Server startup is retried internally by MCPServerManager
- **Runtime Errors** - Any exceptions are caught and logged
- **Shutdown Errors** - Shutdown errors don't prevent application exit

All errors are logged using both:
- `StatusUtil.logError()` - For UI logging
- `log()` - For Eclipse error log with stack traces

## Benefits

✅ **Automatic** - No manual server startup required
✅ **Reliable** - Graceful error handling and recovery
✅ **Integrated** - Follows Eclipse/OSGi lifecycle patterns
✅ **Observable** - Clear logging of all startup/shutdown events
✅ **Scalable** - Ready for AI model connections immediately at startup

## Configuration

The MCP server uses default configuration:
- Server Name: "MyTourBook"
- Server Version: "1.0.0"
- Thread Pool Size: 2 threads
- Startup: Automatic on app launch
- Shutdown: Automatic on app close

For custom configuration, modify values in `MCPServerManager.java`:
```java
private static final String SERVER_NAME = "MyTourBook";
private static final String SERVER_VERSION = "1.0.0";
```

## Accessing the Server

The MCP server can be accessed anywhere in the application:

```java
MCPServerManager manager = MCPServerManager.getInstance();
boolean isRunning = manager.isServerRunning();
```

## Next Steps

1. Build and run MyTourBook
2. Monitor the console log for startup messages
3. The MCP server will be automatically ready for AI model connections
4. No additional configuration or manual startup is required

## Files Modified

- `src/net/tourbook/application/TourbookPlugin.java` - Added MCP server startup/shutdown code

## Files Referenced

- `src/net/tourbook/importdata/MCPServerManager.java` - Server lifecycle management
- `src/net/tourbook/importdata/MCPServerIntegration.java` - Tool and resource integration

## Troubleshooting

If the server fails to start:

1. Check the application log for error messages
2. Verify the io.modelcontextprotocol.sdk library is in the classpath
3. Check system resources (ports, memory)
4. Review MCPServerManager.initializeServer() implementation

If the server doesn't shut down cleanly:

1. It will still be shut down (all resources released)
2. Error message will be logged for debugging
3. Application will still exit normally
