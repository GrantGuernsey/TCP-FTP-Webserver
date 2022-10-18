/**
 * Assignment 1
 * Grant Guernsey
 */

import java.io.*;
import java.net.* ;
import java.nio.charset.StandardCharsets;
import java.util.* ;


public final class WebServer
{
        public static void main(String argv[]) throws Exception
        {
            int port = 6789;
            ServerSocket serverSocket = new ServerSocket(port);

            while(true){
                Socket connection = serverSocket.accept();

                // Construct an object to process the HTTP request message.
                HttpRequest request = new HttpRequest(connection);
                // Create a new thread to process the request.
                Thread thread = new Thread(request);
                // Start the thread.
                thread.start();
            }

        }
}
final class HttpRequest extends FtpClient implements Runnable
{
        final static String CRLF = "\r\n";
        Socket socket;
        // Constructor
        public HttpRequest(Socket socket) throws Exception 
        {
                this.socket = socket;
        }
        // Implement the run() method of the Runnable interface.
        public void run()
        {
            try {
                processRequest();
            } catch (Exception e) {
                    System.out.println(e);
            }
        }
        private void processRequest() throws Exception
        {
                // Get a reference to the socket's input and output streams.
                InputStream is = new DataInputStream(this.socket.getInputStream());
                DataOutputStream os = new DataOutputStream(this.socket.getOutputStream());
                // Set up input stream filters.
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String requestLine = br.readLine();
                // Display the request line.
                System.out.println();
                

                // Extract the filename from the request line.
                StringTokenizer tokens = new StringTokenizer(requestLine);
                tokens.nextToken();  // skip over the method, which should be "GET"
                String fileName = tokens.nextToken();
                // Prepend a "." so that file request is within the current directory.
                fileName = "." + fileName;
                
                // Print out the request message
                System.out.println("Request: ");
                System.out.println(requestLine);
                String headerLine = null;
                while ((headerLine = br.readLine()).length() != 0) {
                        System.out.println(headerLine);
                }

                // Open the requested file.
                FileInputStream fis = null;
                boolean fileExists = true;
                try {
                        fis = new FileInputStream(fileName);
                } catch (FileNotFoundException e) {
                        fileExists = false;
                }
                
                // Construct the response message.
                String statusLine = null;
                String contentTypeLine = null;
                String entityBody = null;
                if (fileExists) {
                        statusLine = "HTTP/1.1 200 OK" + CRLF;
                        contentTypeLine = "Content-type: " + 
                                contentType( fileName ) + CRLF;
                        
                } else {
                        // If the file is a not a text file that does not exist
                        if(!contentType(fileName).equalsIgnoreCase("text/html")
                            && !fileName.endsWith(".txt")){
                                statusLine = "HTTP/1.1 404 Not Found" + CRLF;
                                contentTypeLine = "Content-type: " + 
                                        "text/html" + CRLF;
                                entityBody = "<HTML>" + 
                                        "<HEAD><TITLE>Not Found</TITLE></HEAD>" +
                                        "<BODY>Not Found</BODY></HTML>";
                        }
                        else{
                                // Calling to the ftp server
                                statusLine = "HTTP/1.1 200 OK" + CRLF;
                                contentTypeLine = "Content-type: " + 
                                        contentType(fileName) + CRLF;
                                
                                // Creates the client
                                FtpClient ftp = new FtpClient();
                                // Connects to the client
                                ftp.connect("grant", "guernsey");
                                // Retrieves the specified file
                                ftp.getFile(fileName);
                                // Disconnects from the client
                                ftp.disconnect();
                                
                                // Sets the file input stream as the new found and transferred file
                                fis = new FileInputStream(fileName);
                                fileExists = true;
                        }
                        
                }

                // Send the status line.
                
                os.writeBytes(statusLine);

                // // Send the content type line.
                os.writeBytes(contentTypeLine);

                // // Send a blank line to indicate the end of the header lines.
                os.writeBytes(CRLF);
                System.out.println();
                System.out.print("Response: ");
                // Send the entity body.
                if (fileExists) {
                    // Prints out the response message and sends the file stream to send bytes which sends it 
                    System.out.println(statusLine + contentTypeLine + CRLF + fileName);
                    fis = new FileInputStream(fileName);
                    sendBytes(fis, os);
                    fis.close();
                } else {
                    // Writes the fail body and also prints the response message
                    os.writeBytes(entityBody);
                    System.out.println(statusLine + contentTypeLine + CRLF + "Not found");
                }
                
                
                os.close();
                br.close();
                socket.close();
        }

        private static void sendBytes(FileInputStream fis, OutputStream os) 
        throws Exception
        {
            // Construct a 1K buffer to hold bytes on their way to the socket.
            byte[] buffer = new byte[1024];
            int bytes = 0;
            // Copy requested file into the socket's output stream.
            while((bytes = fis.read(buffer)) != -1 ) {
                os.write(buffer, 0, bytes);
            }
        }

        private static String contentType(String fileName)
        {
                if(fileName.endsWith(".htm") || fileName.endsWith(".html") || fileName.endsWith(".txt")) {
                        return "text/html";
                }
                if(fileName.endsWith(".gif")) {
                        return "image/gif";
                }
                if(fileName.endsWith(".jpg")) {
                        return "image/jpeg";
                }
                return "application/octet-stream";
        }
}