// WARNING: DO NOT MODIFY HARDWARE
// --------------------

import java.util.Hashtable;

class Disk {
    static final int NUM_SECTORS = 2048;
    static final int DISK_DELAY = 800;  // 80 for Gradescope

    StringBuffer sectors[] = new StringBuffer[NUM_SECTORS];

    void write(int sector, StringBuffer data){

    }  // call sleep
    void read(int sector, StringBuffer data){
        
    }   // call sleep

}

class Printer {

    static final int PRINT_DELAY = 2750; // 275 for Gradescope
    void print(StringBuffer data){

    }  // call sleep

}

class FileInfo {
    int diskNumber; 
    int startingSector; 
    int fileLength;
}

// Threads
// --------------------
void processUserCommands(String f){
    System.out.println(f);
}

class UserThread extends Thread{

    String fileName;
    String line; 

    UserThread(String f) {
        fileName = f;
    }

    public void run() {
        processUserCommands(fileName); 
    }
}

class PrinterJobThread extends Thread{

}

// Managers
// --------------------
class DirectoryManager{
    private Hashtable<String, FileInfo> T = new Hashtable<String, FileInfo>();
    
    void enter(StringBuffer fileName, FileInfo file){

    }
    FileInfo lookup(StringBuffer fileName){
        return new FileInfo();
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
    DirectoryManager directoryManager = new DirectoryManager();
    
    DiskManager(int numberOfDisk) {
        super(numberOfDisk);
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
        users = new UserThread[App.NUM_USERS]; 
        for(int i = 0; i < users.length; i++){
            users[i] = new UserThread(); 
        }
        diskManager = new DiskManager(App.NUM_DISK);
        printerManager = new PrinterManager(App.NUM_PRINTERS);
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
            } catch (Exception e) {

            }
        }
    }
}


public class App {
    public static int NUM_USERS = 4, NUM_DISK = 2, NUM_PRINTERS = 3;

    public static void main(String[] args){
        //
        if (args.length > 0){
            for (var arg : args){
                System.err.println(arg);
            }
        }

        OS os = OS.getInstance(); 
        os.startUserThreads();
        os.joinUserThreads();
    }
}
