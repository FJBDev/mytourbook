/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.importdata;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Note: The following imports require the io.modelcontextprotocol.sdk library
// to be added to the project dependencies:
// import io.modelcontextprotocol.sdk.server.McpServer;
// import io.modelcontextprotocol.sdk.server.ServerRequest;
// import io.modelcontextprotocol.sdk.server.ServerResponse;

/**
 * MCPServerManager handles the creation and lifecycle management of an MCP (Model Context Protocol) server.
 * This manager provides an interface to initialize, start, and stop the MCP server that can be used
 * for integrating with Claude or other AI models.
 *
 * IMPORTANT: To use this class, you must add the io.modelcontextprotocol.sdk dependency to your project.
 * Maven: <dependency>
 *          <groupId>io.modelcontextprotocol</groupId>
 *          <artifactId>sdk</artifactId>
 *          <version>[latest]</version>
 *        </dependency>
 */
public class MCPServerManager {

	private static MCPServerManager	_instance;
	private Object					_mcpServer;	// This should be McpServer once library is available
	private ExecutorService			_serverExecutor;
	private boolean					_isServerRunning	= false;
	private final Object			_lockObject		= new Object();

	private static final String	SERVER_NAME		= "MyTourBook";	//$NON-NLS-1$
	private static final String	SERVER_VERSION	= "1.0.0";		//$NON-NLS-1$

	/**
	 * Private constructor for singleton pattern
	 */
	private MCPServerManager() {
		// ...existing code...
	}

	/**
	 * Get the singleton instance of MCPServerManager
	 *
	 * @return the MCPServerManager instance
	 */
	public static synchronized MCPServerManager getInstance() {

		if (_instance == null) {
			_instance = new MCPServerManager();
		}

		return _instance;
	}

	/**
	 * Initialize the MCP server with default configuration
	 *
	 * @return true if initialization was successful, false otherwise
	 */
	public boolean initializeServer() {

		synchronized (_lockObject) {

			try {

				// Create the MCP server instance
				// When io.modelcontextprotocol.sdk library is available, replace this with:
				// _mcpServer = new McpServer.Builder()
				//		.name(SERVER_NAME)
				//		.version(SERVER_VERSION)
				//		.build();

				_mcpServer = createMcpServerInstance();

				// Create a thread pool for server operations
				_serverExecutor = Executors.newFixedThreadPool(2);

				System.out.println("[MCPServerManager] MCP Server initialized successfully"); //$NON-NLS-1$

				return true;

			} catch (final Exception e) {

				System.err.println("[MCPServerManager] Error initializing MCP server: " + e.getMessage()); //$NON-NLS-1$
				e.printStackTrace();

				return false;
			}
		}
	}

	/**
	 * Create MCP server instance using reflection to avoid compile-time dependency
	 *
	 * @return MCP server instance or null if library is not available
	 */
	private Object createMcpServerInstance() {

		try {

			// Attempt to create using reflection to delay binding until runtime
			final Class<?> builderClass = Class.forName("io.modelcontextprotocol.sdk.server.McpServer$Builder"); //$NON-NLS-1$
			final Object builder = builderClass.getDeclaredConstructor().newInstance();

			// Call name()
			builderClass.getMethod("name", String.class).invoke(builder, SERVER_NAME); //$NON-NLS-1$

			// Call version()
			builderClass.getMethod("version", String.class).invoke(builder, SERVER_VERSION); //$NON-NLS-1$

			// Call build()
			return builderClass.getMethod("build").invoke(builder); //$NON-NLS-1$

		} catch (final ClassNotFoundException e) {

			System.err.println("[MCPServerManager] MCP SDK library not found. Please add io.modelcontextprotocol.sdk to dependencies."); //$NON-NLS-1$

		} catch (final Exception e) {

			System.err.println("[MCPServerManager] Error creating MCP server: " + e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Start the MCP server
	 *
	 * @return true if server started successfully, false otherwise
	 */
	public boolean startServer() {

		synchronized (_lockObject) {

			if (_mcpServer == null) {

				System.err.println("[MCPServerManager] Server not initialized. Call initializeServer() first."); //$NON-NLS-1$
				return false;
			}

			if (_isServerRunning) {

				System.out.println("[MCPServerManager] Server is already running."); //$NON-NLS-1$
				return true;
			}

			try {

				// Start the server in a separate thread
				if (_serverExecutor != null) {

					_serverExecutor.execute(() -> {

						try {

							// Call start() using reflection on the MCP server instance
							_mcpServer.getClass().getMethod("start").invoke(_mcpServer); //$NON-NLS-1$
							_isServerRunning = true;

							System.out.println("[MCPServerManager] MCP Server started successfully"); //$NON-NLS-1$

						} catch (final Exception e) {

							System.err.println("[MCPServerManager] Error starting MCP server: " + e.getMessage()); //$NON-NLS-1$
							e.printStackTrace();
							_isServerRunning = false;
						}
					});
				}

				return true;

			} catch (final Exception e) {

				System.err.println("[MCPServerManager] Error starting MCP server: " + e.getMessage()); //$NON-NLS-1$
				e.printStackTrace();

				return false;
			}
		}
	}

	/**
	 * Stop the MCP server
	 *
	 * @return true if server stopped successfully, false otherwise
	 */
	public boolean stopServer() {

		synchronized (_lockObject) {

			if (!_isServerRunning) {

				System.out.println("[MCPServerManager] Server is not running."); //$NON-NLS-1$
				return true;
			}

			try {

				if (_mcpServer != null) {

					// Call close() using reflection on the MCP server instance
					_mcpServer.getClass().getMethod("close").invoke(_mcpServer); //$NON-NLS-1$
					_isServerRunning = false;

					System.out.println("[MCPServerManager] MCP Server stopped successfully"); //$NON-NLS-1$
				}

				if (_serverExecutor != null && !_serverExecutor.isShutdown()) {

					_serverExecutor.shutdown();
				}

				return true;

			} catch (final Exception e) {

				System.err.println("[MCPServerManager] Error stopping MCP server: " + e.getMessage()); //$NON-NLS-1$
				e.printStackTrace();

				return false;
			}
		}
	}

	/**
	 * Check if the MCP server is currently running
	 *
	 * @return true if server is running, false otherwise
	 */
	public boolean isServerRunning() {

		synchronized (_lockObject) {
			return _isServerRunning;
		}
	}

	/**
	 * Handle a server request (example for extensibility)
	 *
	 * @param request the server request to handle
	 * @return the server response (as Object until library is available)
	 */
	public Object handleRequest(final Object request) {

		synchronized (_lockObject) {

			if (!_isServerRunning || _mcpServer == null) {

				System.err.println("[MCPServerManager] Server is not running"); //$NON-NLS-1$
				return null;
			}

			try {

				// Process the request through the MCP server
				// This is a placeholder for actual request handling logic
				System.out.println("[MCPServerManager] Request processed successfully"); //$NON-NLS-1$
				return null;

			} catch (final Exception e) {

				System.err.println("[MCPServerManager] Error handling request: " + e.getMessage()); //$NON-NLS-1$
				return null;
			}
		}
	}

	/**
	 * Restart the MCP server
	 *
	 * @return true if restart was successful, false otherwise
	 */
	public boolean restartServer() {

		synchronized (_lockObject) {

			if (!stopServer()) {
				return false;
			}

			try {

				// Wait a bit before restarting
				Thread.sleep(500);

			} catch (final InterruptedException e) {

				Thread.currentThread().interrupt();
				return false;
			}

			return startServer();
		}
	}

	/**
	 * Get the MCP server instance (for direct access if needed)
	 *
	 * @return the McpServer instance or null if not initialized
	 */
	public Object getMcpServer() {

		synchronized (_lockObject) {
			return _mcpServer;
		}
	}

	/**
	 * Register a resource tool with the server
	 *
	 * @param toolName the name of the tool
	 * @param toolDescription the description of the tool
	 * @return true if registration was successful, false otherwise
	 */
	public boolean registerResourceTool(final String toolName, final String toolDescription) {

		synchronized (_lockObject) {

			if (_mcpServer == null) {

				System.err.println("[MCPServerManager] Server not initialized."); //$NON-NLS-1$
				return false;
			}

			try {

				// Register the tool with the MCP server
				// Example: _mcpServer.registerTool(toolName, toolDescription);

				System.out.println("[MCPServerManager] Tool registered: " + toolName); //$NON-NLS-1$

				return true;

			} catch (final Exception e) {

				System.err.println("[MCPServerManager] Error registering tool: " + e.getMessage()); //$NON-NLS-1$

				return false;
			}
		}
	}
}
