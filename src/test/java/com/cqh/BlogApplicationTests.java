package com.cqh;

import com.cqh.dao.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

// NOTE: This is a Spring Boot smoke test that loads the full ApplicationContext,
// which requires a reachable MySQL server (BlogRepository.findGroupYear() uses
// MySQL's date_format() function so H2/embedded substitution does not work).
// It runs locally when MySQL is up; CI excludes it via -Dtest='!BlogApplicationTests'.
@RunWith(SpringRunner.class)
@SpringBootTest
public class BlogApplicationTests {

	@Autowired
	private UserRepository userRepository;

	@Test
	public void contextLoads() {

	}

}
