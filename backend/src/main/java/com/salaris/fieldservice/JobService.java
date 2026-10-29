import java.util.Objects;

public class JobService {

    public void setWorker(Job job) {
        System.out.println("Worker set for job: " + job.getName());
    }

    public void updateEquipment(Job job) {
        // Placeholder for equipment update logic.
        System.out.println("Updating equipment for job: " + job.getName());
        JobInventoryService jobInventoryService = new JobInventoryService();
        jobInventoryService.populateJobWithEquipment(job.getId());
        job.setEquipment(jobInventoryService.getTruckInventory(job.getId()));
    }

}

