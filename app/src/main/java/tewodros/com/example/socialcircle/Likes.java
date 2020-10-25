package tewodros.com.example.socialcircle;

public class Likes {

    String fullname,profileimage;

    public Likes(){

    }

    public Likes(String fullname, String profileimage) {
        this.fullname = fullname;
        this.profileimage = profileimage;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getProfileimage() {
        return profileimage;
    }

    public void setProfileimage(String profileimage) {
        this.profileimage = profileimage;
    }
}
