class Test30
{
	static class Student
	{
		String name;
		Course[] enrolledCourse;
		int courseCount;

	}
	static class Course
	{
		String courseName;
		int credits;
		Professor prof;
		Course(String c, int d)
		{
			courseName=c;
			credits=d;
		}
	}
	static class Professor
	{
		String name;
		Professor(String p)
		{
			name=p;
		}
	}
	static Course test()
	{
		Student s1=new Student();
		s1.courseCount = 4;
		s1.name = "A";
		s1.enrolledCourse = new Course[4];

		Student s2 = new Student();
		s2.courseCount = 4;
		s2.name = "B";
		s2.enrolledCourse = new Course[4];

		Course c1=new Course("C1",6);

		s1.enrolledCourse[s1.courseCount++]=c1;
		s2.enrolledCourse[s2.courseCount++]=c1;
		Professor p1=new Professor("P1");
		c1.prof=p1;
		Course c2=new Course("C2",4);
		Professor p2=new Professor("P2");
		c2.prof=p2;
		s2.enrolledCourse[s2.courseCount++]=c2;

		Course c3=new Course("C3",6);
		c3.prof=p2;
		return c3;

	}

	public static void main(String[] args) {
		Course c=test();
	}

}

//45,49
