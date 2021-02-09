/// Project 2 to submit
package serv;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import helper.ChunkFileObject;
public class Server_P {

	static String RootFileLocation = "C:/P2P_File";
	static String BaseFileLocation = RootFileLocation + "/Server";
	
	Socket SocketConnection;
	ServerSocket SocketReceive;
	
	int ChunkSize = 102400;
	private int OwnerServerPort;
	// have both these in sync
	static Map<Integer, ArrayList<Integer>> clientMap;//""delete
	static Map<Integer, String> clientMapConf;//* client port number?

	public static void main(String[] args) {

		Server_P Server = new Server_P();
		try {
			
			// break the input file into chunks
			Server.divideInputFile();
			// read conf file
			Server.readPortValues();//"""delete
			String str;
			int clients = 0;
			clientMapConf = new LinkedHashMap<Integer, String>();//*del
			BufferedReader br = new BufferedReader(new FileReader(RootFileLocation + "/config.txt"));//*del
			//first line for server
			br.readLine();
			
 			while ((str = br.readLine()) != null)//*del
				clientMapConf.put(++clients, str);

 			String ChunkLocation = BaseFileLocation + "/chunks";
			File[] ChunkFiles = new File(ChunkLocation).listFiles();//*listFiles: list every ChunkFiles in location

			int Count_Chunk = ChunkFiles.length;
      			System.out.println("Total number of Chunks:" + Count_Chunk);

			clientMap = new LinkedHashMap<Integer, ArrayList<Integer>>();//*?
 			for (int i = 1; i <= clients; i++) {
				ArrayList<Integer> arr = new ArrayList<Integer>();
     				for (int j = i; j <= Count_Chunk; j += clients) {
  					arr.add(j);
				}
				clientMap.put(i, arr);

			}
			System.out.println(clientMap);

			if (Count_Chunk > 0) {
				// wait for the connections to initiate
				Server.ServerConnection(ChunkFiles);
			} else
				System.out.println("There are no ChunkFiles in chunks folder!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void ServerConnection(File[] ChunkFiles) {
		try {
			int client = 0; // initialized to 0
			SocketReceive = new ServerSocket(OwnerServerPort);
			System.out.println("MainServer socket was created. Accept connection.");
			while (true) {
				System.out.println(clientMap);
				client++;
				//*ClientMap size is total file size after being divided?
				if (client <= clientMap.size()) {
					SocketConnection = SocketReceive.accept();
					System.out.println("new client connection accepted :-" + SocketConnection);
					// create thread and pass the socket n ChunkFiles to handle
					new ServerThread(SocketConnection, ChunkFiles, clientMap.get(client), clientMapConf.get(client)).start();

					/////???
				} else {
					System.out.println("No more clients, I am done!");
					break;
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void readPortValues() throws FileNotFoundException
	{
		String str=null;
		
		BufferedReader br = new BufferedReader(new FileReader(RootFileLocation + "/config.txt"));
		try {
			str = br.readLine();
			String[] tokens = str.split(" ");
			OwnerServerPort = Integer.parseInt(tokens[1]);//??
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void divideInputFile() {

		try {
			//xyz.mp4
			// main file that is to be broken into pieces
			Scanner sc=new Scanner(System.in);
			System.out.println("Please place the file in c:p2p folder and Enter the filename:");
			String input=sc.nextLine();
			 //"FLR.pdf"
			File inputFile = new File(BaseFileLocation +"/"+input);
			Long fileLength = inputFile.length();//size of file

			System.out.println("Input File size :" + fileLength);

			String newdir = inputFile.getParent() + "/chunks/";
			File outFolder = new File(newdir);
			if (outFolder.mkdirs())//create new folder
				System.out.println("Chunks Folder created");
			else
				System.out.println("Chunks folder already exits or unable to create folder for chunks");

			byte[] chunk = new byte[102400];

			FileInputStream fileInStream = new FileInputStream(inputFile);

			BufferedInputStream bufferStream = new BufferedInputStream(fileInStream);
			int index = 1;
			int bytesRead;
			// chuck will be populated with data
			
			////????
			while ((bytesRead = bufferStream.read(chunk)) > 0) {
				FileOutputStream fileOutStream = new FileOutputStream(
						new File(newdir, String.format("%04d", index) + "_" + inputFile.getName()));
				BufferedOutputStream bufferOutStream = new BufferedOutputStream(fileOutStream);
				bufferOutStream.write(chunk, 0, bytesRead);
				bufferOutStream.close();
				index++;
			}
			bufferStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

class ServerThread extends Thread {

	private Socket socket;
	File[] ChunkFiles;
	ObjectOutputStream outStream;
	ArrayList<Integer> chunkList;
	String configClient;// port number?

	ServerThread(Socket Server, File[] ChunkFiles, ArrayList<Integer> cl, String str) {
		this.socket = Server;
		this.ChunkFiles = ChunkFiles;
		this.chunkList = cl;
		this.configClient = str;
	}

	public void run() {
		try {
			// get output stream
			outStream = new ObjectOutputStream(socket.getOutputStream());
			//**initialize inputStream and outputStream
			//**send ChunkFiles, config and so on message
			/*
			 * Total no of ChunkFiles clients need to have... info will be passed to
			 * each client from here
			 */
			outStream.writeObject(configClient);

			outStream.writeObject(ChunkFiles.length);//*ChunkFiles: list on location, length = total amount of ChunkFiles

			outStream.writeObject(chunkList.size());//*chunklist: [1,2,...40], size=40
			Arrays.sort(ChunkFiles);
			for (int i = 0; i < chunkList.size(); i++) {
				// construct the chunk object
				ChunkFileObject sChunkObj = constructChuckFileObject(ChunkFiles[chunkList.get(i) - 1], chunkList.get(i));//**??
				// send file
				sendChunkObject(sChunkObj);
				// let's intro sleep
//				//Thread.sleep(1000);
			}
			// disconnect
			TCPServDisconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ChunkFileObject constructChuckFileObject(File file, int chunkNum) throws IOException {
		byte[] chunk = new byte[102400]; // should be 100kb, see demo
		System.out.println("construct object - " + file.getName());
		ChunkFileObject chunkObj = new ChunkFileObject();

		chunkObj.setFileNum(chunkNum);

		chunkObj.setFileName(file.getName());
		FileInputStream fileInStream = new FileInputStream(file);

		BufferedInputStream bufferInStream = new BufferedInputStream(fileInStream);

		int bytesRead = bufferInStream.read(chunk);

		chunkObj.setChunksize(bytesRead);

		chunkObj.setFileData(chunk);

		bufferInStream.close();
		fileInStream.close();

		return chunkObj;
	}

	public void sendChunkObject(ChunkFileObject sChunkObj) {
		try {

			outStream.writeObject(sChunkObj);
			outStream.flush();
			System.out.println("send object done...");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void TCPServDisconnect() {
		try {
			outStream.close();
			//System.out.println("file out stream closed...");
			socket.close();
			System.out.println("Main Server socket closed:" + socket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
