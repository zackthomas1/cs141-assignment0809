import java.util.Hashtable;
import java.io.*;

// WARNING: DO NOT MODIFY HARDWARE
// --------------------
class Disk {
    static final int NUM_SECTORS = 2048;
    static final int DISK_DELAY = 800;  // 80 for Gradescope

    StringBuffer sectors[] = new StringBuffer[NUM_SECTORS];

    Disk() {}

    void write(int sector, StringBuffer data){
        // call sleep
        try{
            Thread.sleep(DISK_DELAY);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        sectors[sector] = data;
    }
    void read(int sector, StringBuffer data){
        // call sleep
        try{
            Thread.sleep(DISK_DELAY);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        data.delete(0, data.length());
        data.append(sectors[sector]);
    }
}

class Printer {
    static final int PRINT_DELAY = 2750; // 275 for Gradescope
    
    Printer(int id) {}
    
    void print(StringBuffer data){
        // call sleep
        try{
            Thread.sleep(PRINT_DELAY);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        System.out.println(data.toString());
    }

}

class FileInfo {
    int diskNumber; 
    int startingSector; 
    int fileLength;
}

// Threads
// --------------------
class UserThread extends Thread{
    String fileName;
    String line; 

    UserThread(String f) {
        super();
        fileName = f;
    }

    @Override
    public void run() {
        processUserCommands(fileName); 
    }
    void processUserCommands(String f){
        System.out.println(f);
        OS os = OS.getInstance();

        try {
            // initialize reader
            FileInputStream inputStream = new FileInputStream(f);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(inputStream));

            // read file
            for(String line; (line = myReader.readLine()) != null;){
                String[] cmds = line.split("\\s+");
                switch(cmds[0]){
                    case ".save":
                        System.out.printf("command: Save argument: %s\n", cmds[1]);
                        saveFile(cmds[1]);
                        break;
                    case ".end":
                        System.out.printf("command: End\n");
                        break;
                    case ".print":
                        System.out.printf("command: Print argument: %s\n", cmds[1]);
                        printFile(cmds[1]);
                        break; 
                    default: 
                        System.out.printf("command: Print argument: %s\n", cmds[1]);
                        break;
                }
            }
            // close reader
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    saveFile(String file){
        System.out.println();
    }

    printFile(String file){

    }
}

class PrinterJobThread extends Thread{
    PrinterJobThread() {
        super();
    }

    @Override
    public void run() {

    }
}

// Managers
// --------------------
class DirectoryManager{
    private Hashtable<String, FileInfo> T = new Hashtable<String, FileInfo>();
    
    void enter(StringBuffer fileName, FileInfo file){

    }

    FileInfo lookup(StringBuffer fileName){
        return null;
    }
}

class ResourceManager{
    boolean isFree[]; 
    
    ResourceManager(int numberOfItems){
        isFree = new boolean[numberOfItems]; 
        for(int i = 0; i < isFree.length; i++){
            isFree[i] = true; 
        }
    }

    synchronized int request () {
        while (true) {
            for (int i = 0; i < isFree.length; i++) {
                if (isFree[i]) {
                    isFree[i] = false;
                    return i;
                }
                try {
                    this.wait(); // block until someone releases resource
                } catch (InterruptedException e) {

                }
            }
        }
    }

    synchronized void release (int index) {
        isFree[index] = true; 
        this.notify();  // let a blocked thread run
    }
}

class DiskManager extends ResourceManager{
    Disk disks[]; 
    DirectoryManager directoryManager;
    
    DiskManager(int numberOfDisk) {
        super(numberOfDisk);
        directoryManager = new DirectoryManager();
        disks = new Disk[numberOfDisk];
    }
}

class PrinterManager extends ResourceManager{
    Printer printers[]; 

    PrinterManager(int numberOfPrinters) {
        super(numberOfPrinters);
        printers = new Printer[numberOfPrinters];
    }
}

// OS
// --------------------
class OS {
    public static int NUM_USERS = 1, NUM_DISK = 1, NUM_PRINTERS = 1;
    public static String INPUT_DIR = "docs/input/";
    private static OS instance;

    public static OS getInstance() {
        if(instance == null){
            instance = new OS(); 
        }    
        return instance;
    }

    UserThread users[]; 
    DiskManager diskManager; 
    PrinterManager printerManager; 

    private OS() {
        users = new UserThread[OS.NUM_USERS]; 
        for(int i = 0; i < users.length; i++){
            String fname = OS.INPUT_DIR + "User" + i;
            users[i] = new UserThread(fname); 
        }
        diskManager = new DiskManager(OS.NUM_DISK);
        printerManager = new PrinterManager(OS.NUM_PRINTERS);
    }

    void startUserThreads(){
        for(var user: users){
            user.start(); 
        }
    }

    void joinUserThreads(){
        for(var user: users){
            try {
                user.join();
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }
}


public class App {
    public static void main(String[] args){
        //
        for (int i=0; i<args.length; ++i)
            System.out.println("Args[" + i + "] = " + args[i]);

        System.out.println("*** 141 OS Simulation ***");

        OS os = OS.getInstance(); 
        os.startUserThreads();
        os.joinUserThreads();
    }
}
