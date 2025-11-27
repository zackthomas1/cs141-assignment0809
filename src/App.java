import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

// WARNING: DO NOT MODIFY HARDWARE
// --------------------
class Disk {
    static final int NUM_SECTORS = 2048;
    static final int DISK_DELAY = 80;  // 80 for Gradescope

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
    static final int PRINT_DELAY = 275; // 275 for Gradescope
    
    Printer() {}
    
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

    public FileInfo(int disk, int sector, int length) {
        diskNumber = disk; 
        startingSector = sector; 
        fileLength = length;
    }
}

// Threads
// --------------------
class UserThread extends Thread{
    String commandsFile;

    UserThread(String f) {
        super();
        commandsFile = f;
    }

    @Override
    public void run() {
        processUserCommands(commandsFile); 
    }

    void processUserCommands(String f){
        try (FileInputStream inputStream = new FileInputStream(f);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(inputStream))) {
            // read file
            for(String line ;(line = myReader.readLine()) != null;){
                // check if thread was interrupted
                if(Thread.currentThread().isInterrupted()){
                    System.err.println("Process interrupted");
                    return;
                }

                String[] cmdargs = line.split("\\s+");
                switch(cmdargs[0]) {
                    case ".save":
                        saveFile(cmdargs[1], myReader);
                        break;
                    case ".print":
                        printFile(cmdargs[1]);
                        break; 
                    default: 
                        System.out.printf("Unknown Command: %s", cmdargs[0]);
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
        } catch (IOException e) {
            System.err.println("IO error");
        }
    }

    void saveFile(String file, BufferedReader reader){
        DiskManager dm = OS.getInstance().diskManager;
        
        System.out.printf("SAVE file %s from %s\n", file, commandsFile);
        int diskIndex = dm.request();
        int diskFreeSector = dm.getNextFreeSector(diskIndex);
        int offset = 0;
        
        try {
            for(String line; (line = reader.readLine()) != null;){
                if(".end".equals(line)){
                    dm.directoryManager.enter(file, new FileInfo(diskIndex, diskFreeSector, offset));
                    dm.setNextFreeSector(diskIndex, diskFreeSector + offset);
                    break;
                }else{
                    dm.disks[diskIndex].write(diskFreeSector + offset, new StringBuffer(line));
                    offset++;
                }
            }
        } catch (IOException e) {
            System.err.println("IO error");
        }
        dm.release(diskIndex);
    }

    void printFile(String file){
        PrinterJobThread printerThread = new PrinterJobThread(file); 
        printerThread.start();

        try {
            printerThread.join();
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}

class PrinterJobThread extends Thread{
    String file;
    PrinterJobThread(String f) {
        super();
        file = f;
    }

    @Override
    public void run() {
        System.out.printf("PRINT file %s\n", file);
        DiskManager dm = OS.getInstance().diskManager;
        PrinterManager pm = OS.getInstance().printerManager;

        StringBuffer line = new StringBuffer();
        FileInfo f = dm.directoryManager.lookup(file);
        int start = f.startingSector;
        int diskIndex = f.diskNumber; 
        int printerIndex = pm.request();
        for(int i = 0; i < f.fileLength; i++){
            dm.disks[diskIndex].read(start + i, line);
            pm.printers[printerIndex].print(line);
        }
        pm.release(printerIndex);
    }
}

// Managers
// --------------------
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
                    Thread.currentThread().interrupt();
                    System.err.println("Thread interrupted");
                    return -1;
                }
            }
        }
    }

    synchronized void release (int index) {
        isFree[index] = true; 
        this.notify();  // let a blocked thread run
    }
}

class DirectoryManager{
    private final ConcurrentHashMap<String, FileInfo> T = new ConcurrentHashMap<>();
    
    void enter(String fileName, FileInfo fileinfo){
        T.put(fileName, fileinfo);
    }

    FileInfo lookup(String fileName){
        return T.get(fileName);
    }
}

class DiskManager extends ResourceManager{
    Disk disks[]; 
    int freeSectors[];
    DirectoryManager directoryManager;
    
    DiskManager(int numberOfDisk) {
        super(numberOfDisk);
        directoryManager = new DirectoryManager();
        disks = new Disk[numberOfDisk];
        for(int i = 0; i < disks.length; i++){ 
            disks[i] = new Disk(); 
        }
        freeSectors = new int[numberOfDisk];
    }

    void setNextFreeSector(int d, int offset){ 
        freeSectors[d] = offset;
    }

    int getNextFreeSector(int d){
        return freeSectors[d];
    }
}

class PrinterManager extends ResourceManager{
    Printer printers[]; 

    PrinterManager(int numberOfPrinters) {
        super(numberOfPrinters);
        printers = new Printer[numberOfPrinters];
        for(int i = 0; i < numberOfPrinters; i++){
            printers[i] = new Printer();
        }
    }
}

// OS
// --------------------
class OS {
    public static int NUM_USERS = 3, NUM_DISK = 1, NUM_PRINTERS = 1;
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
